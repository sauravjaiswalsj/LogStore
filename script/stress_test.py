import requests
import time
import json
from concurrent.futures import ThreadPoolExecutor, as_completed

# Configuration
API_ENDPOINT = "http://localhost:8080/append"
NUM_REQUESTS = 10000  # Total number of requests to send
CONCURRENCY = 100    # Number of concurrent requests
PAYLOAD_SIZE_KB = 2 # Size of the random data payload in KB

def generate_random_string(length_kb):
    """Generates a random string of specified length in KB."""
    # Each character is roughly 1 byte, so length_kb * 1024 bytes
    return 'a' * (length_kb * 1024)

def send_request(request_id):
    """Sends a single POST request to the API endpoint."""
    payload = {
        "key": f"test-key-{request_id}-{int(time.time() * 1000)}", # Unique key
        "value": generate_random_string(PAYLOAD_SIZE_KB)
    }
    headers = {
        "Content-Type": "application/json"
    }
    start_time = time.time()
    try:
        response = requests.post(API_ENDPOINT, data=json.dumps(payload), headers=headers)
        end_time = time.time()
        latency = (end_time - start_time) * 1000  # in ms
        if response.status_code == 200:
            return True, latency, f"Request {request_id} successful ({latency:.2f}ms)"
        else:
            return False, latency, f"Request {request_id} failed with status {response.status_code}: {response.text}"
    except requests.exceptions.RequestException as e:
        end_time = time.time()
        latency = (end_time - start_time) * 1000  # in ms
        return False, latency, f"Request {request_id} failed with exception: {e}"

def run_stress_test():
    """Runs the stress test with configured parameters."""
    print(f"Starting stress test for {API_ENDPOINT}...")
    print(f"Total requests: {NUM_REQUESTS}, Concurrency: {CONCURRENCY}, Payload size: {PAYLOAD_SIZE_KB}KB")

    successful_requests = 0
    total_latency = 0
    failed_requests_details = []

    start_test_time = time.time()

    with ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
        futures = [executor.submit(send_request, i + 1) for i in range(NUM_REQUESTS)]

        for future in as_completed(futures):
            success, latency, message = future.result()
            if success:
                successful_requests += 1
                total_latency += latency
            else:
                failed_requests_details.append(message)
            # print(message) # Uncomment for verbose output

    end_test_time = time.time()
    total_test_duration = end_test_time - start_test_time

    print("\n--- Test Results ---")
    print(f"Total requests sent: {NUM_REQUESTS}")
    print(f"Successful requests: {successful_requests}")
    print(f"Failed requests: {NUM_REQUESTS - successful_requests}")
    if successful_requests > 0:
        average_latency = total_latency / successful_requests
        print(f"Average successful latency: {average_latency:.2f} ms")
    print(f"Total test duration: {total_test_duration:.2f} seconds")
    if total_test_duration > 0:
        throughput = successful_requests / total_test_duration
        print(f"Throughput: {throughput:.2f} requests/second")

    if failed_requests_details:
        print("\n--- Failed Request Details ---")
        for detail in failed_requests_details:
            print(detail)

if __name__ == "__main__":
    run_stress_test()
