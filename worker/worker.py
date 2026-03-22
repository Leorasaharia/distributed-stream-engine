import os
from celery import Celery

broker_url = os.environ.get("CELERY_BROKER_URL", "redis://localhost:6379/0")
result_backend = os.environ.get("CELERY_RESULT_BACKEND", "redis://localhost:6379/1")

app = Celery('worker', broker=broker_url, backend=result_backend, include=['tasks'])

app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='UTC',
    enable_utc=True,
    task_acks_late=True, # Guarantee at-least-once execution
    worker_prefetch_multiplier=1 # Backpressure handling: limits the number of tasks pre-fetched by the worker
)

if __name__ == '__main__':
    app.start()
