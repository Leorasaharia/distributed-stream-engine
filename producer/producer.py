import json
import time
import random
import sys
import os
from faker import Faker
from kafka import KafkaProducer

fake = Faker()
KAFKA_BROKER = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC = "events-topic"

def get_producer():
    print(f"Attempting to connect to Kafka at {KAFKA_BROKER}")
    for _ in range(15):
        try:
            return KafkaProducer(
                bootstrap_servers=[KAFKA_BROKER],
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
        except Exception as e:
            print(f"Waiting for Kafka to be ready... {e}")
            time.sleep(5)
    print("Could not connect to Kafka. Exiting.")
    sys.exit(1)

def produce_events(producer):
    users = [f"u{i}" for i in range(1, 101)]
    event_types = ["click", "view", "purchase", "login", "scroll"]
    
    print("Starting event generation...")
    while True:
        event = {
            "user_id": random.choice(users),
            "event_type": random.choice(event_types),
            "timestamp": int(time.time()),
            "value": random.randint(1, 100)
        }
        
        producer.send(TOPIC, event)
        print(f"Produced: {event}")
        time.sleep(random.uniform(0.1, 1.0))

if __name__ == "__main__":
    producer = get_producer()
    produce_events(producer)
