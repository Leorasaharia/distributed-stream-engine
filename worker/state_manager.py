import redis
import os
import json

class StateManager:
    def __init__(self):
        # Main redis instance (DB 0 for shared metrics vs DB 1 for celery backend)
        redis_host = os.environ.get("SPRING_REDIS_HOST", "redis")
        redis_port = int(os.environ.get("SPRING_REDIS_PORT", "6379"))
        self.redis = redis.Redis(host=redis_host, port=redis_port, db=0, decode_responses=True)

    def is_processed(self, event_id):
        return self.redis.exists(f"idempotency:{event_id}")

    def mark_processed(self, event_id):
        self.redis.setex(f"idempotency:{event_id}", 86400, "1")  # 24h retention

    def update_user_count(self, user_id):
        self.redis.hincrby("metrics:user_counts", user_id, 1)
        
    def increment_processed_jobs(self):
        self.redis.incr("metrics:jobs_processed")

    def process_window(self, event_data):
        """
        Implements a sliding window aggregation based on event-time.
        Window size: 10 seconds.
        Aggregates the sum of event 'value's for a user sliding window.
        """
        event_time = int(event_data.get('timestamp', 0))
        user_id = event_data['user_id']
        value = event_data.get('value', 0)
        
        # Sliding window key for user
        window_key = f"window:{user_id}:events"
        agg_key = f"window:{user_id}:agg"
        
        # Add event to sorted set: score is event_time
        member_id = f"{user_id}_{event_time}_{value}"
        member_data = json.dumps({"v": value, "id": member_id})
        self.redis.zadd(window_key, {member_data: event_time})
        
        # Remove late/old events (older than 10 seconds from current event_time)
        # This acts as our watermark approach for a 10s sliding window
        cutoff = event_time - 10
        self.redis.zremrangebyscore(window_key, "-inf", cutoff)
        
        # Calculate new aggregate for the window
        events_in_window = self.redis.zrange(window_key, 0, -1)
        total_value = sum([json.loads(e).get('v', 0) for e in events_in_window])
        
        # Store the aggregated result
        self.redis.set(agg_key, total_value)
