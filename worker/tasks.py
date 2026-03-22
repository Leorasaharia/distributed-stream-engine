from worker import app
from state_manager import StateManager
from celery.utils.log import get_task_logger

logger = get_task_logger(__name__)
state_manager = StateManager()

@app.task(bind=True, name="tasks.process_event", max_retries=3)
def process_event(self, event_data):
    """
    Process streaming events with idempotency, event-time semantics,
    sliding windows, and fault tolerance via retries.
    """
    try:
        user_id = event_data.get('user_id')
        event_time = event_data.get('timestamp')
        
        if not user_id or not event_time:
            logger.warning(f"Invalid event data: {event_data}")
            return "Invalid event"

        # 1. Idempotency Check
        # Uses user_id and timestamp as a unique identifier for simplicity
        event_id = f"{user_id}_{event_time}"
        if state_manager.is_processed(event_id):
            logger.info(f"Event {event_id} already processed. Skipping.")
            return "Already processed"
            
        # 2. Update Global Metrics
        state_manager.update_user_count(user_id)
        
        # 3. Process Windowed Aggregations (Sliding Window based on Event Time)
        state_manager.process_window(event_data)
        
        # 4. Mark as processed & increment tracking metrics
        state_manager.mark_processed(event_id)
        state_manager.increment_processed_jobs()
        
        logger.info(f"Successfully processed event {event_id}")
        return "Processed successfully"

    except Exception as exc:
        logger.error(f"Error processing event {event_data}: {exc}")
        # Fault Tolerance: Automatic Retry with Exponential Backoff
        raise self.retry(exc=exc, countdown=2 ** self.request.retries)
