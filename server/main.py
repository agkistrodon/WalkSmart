import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

from flask import Flask, request, jsonify
import numpy as np
import keras.models
import io
import pandas as pd
from scipy import stats

n_time_steps = 500
n_features = 6
step = 50
columns = ['time', 'Ax-axis','Ay-axis', 'Az-axis', 'Gx-axis', 'Gy-axis', 'Gz-axis', 'Gait']

def load_model(model_path):
    model = keras.models.load_model(model_path)
    return model

def sgdata(data):
    print(data.shape)

    segments = []
    labels = []

    for i in range(0,  data.shape[0]- n_time_steps, step):

        Axs = data['Ax-axis'].values[i: i + n_time_steps]
        Ays = data['Ay-axis'].values[i: i + n_time_steps]
        Azs = data['Az-axis'].values[i: i + n_time_steps]

        Gxs = data['Gx-axis'].values[i: i + n_time_steps]
        Gys = data['Gy-axis'].values[i: i + n_time_steps]
        Gzs = data['Gz-axis'].values[i: i + n_time_steps]

        label = stats.mode(data['Gait'][i: i + n_time_steps])[0][0]

        segments.append([Axs, Ays, Azs, Gxs, Gys, Gzs])

        labels.append(label)

    #reshape the segments which is (list of arrays) to a list
    print(len(segments))
    reshaped_segments = np.asarray(segments).reshape(-1, n_time_steps, n_features)
    print(reshaped_segments.shape)
    #labels = np.asarray(pd.get_dummies(labels), dtype = np.float32)
    return reshaped_segments


def predict(csv_bytes, model):
    data = pd.read_csv(io.BytesIO(csv_bytes), names = columns)
    n = 200
    data = data.tail(-n)
    data = data.head(-n)
    data['Gait'] = 'test'
    data = data.dropna()
    data = data.astype({'Ax-axis':'float','Ay-axis':'float', 'Az-axis':'float', 'Gx-axis':'float','Gy-axis':'float', 'Gz-axis':'float' })

    segments = sgdata(data=data)
    prediction = model.predict(segments)

    sum0 = 0.0
    sum1 = 0.0

    for i in prediction:
        sum0 += i[0]
        sum1 += i[1]
    
    return jsonify({'status': 0, 'abnormal-prob': float(sum0) / len(prediction)})


basemodel = load_model('gait-model.h5')

app = Flask(__name__)

@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == "POST":
        csv_file = request.files.get('csv')
        if csv_file is None or csv_file.filename == "":
            return jsonify({ "status": 1, "error": "no file" })
        try:
            csv_bytes = csv_file.read()
            return predict(csv_bytes=csv_bytes, model=basemodel)
        except Exception as e:
            return jsonify({ "status": 1, "error": str(e) })
    else:
        return "OK"

if __name__ == "__main__":
    app.run(debug=True)
