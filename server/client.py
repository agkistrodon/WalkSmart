import requests
import sys

path = sys.argv[1]

csv_file = open(path, 'rb')

print(csv_file)
url = 'http://127.0.0.1:5000'

files = {'csv': csv_file}

response = requests.post(url, files=files)

if response.status_code == 200:
    response_data = response.json()
    print(response_data)
else:
    print("Error: ", response.status_code, response.text)
