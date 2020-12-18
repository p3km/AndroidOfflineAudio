from flask import Flask, send_file, request, send_from_directory
from algorithm import run

import sys
import os


app = Flask(__name__)


@app.route("/", methods = ["GET"])
def main_page():
    return "Welcome to the Main Page"

#
@app.route("/Downloads/<name>")
def send_audio(name):
    try:
        # file must already be on server for it to be sent
        return send_from_directory(os.getcwd(), filename=name+".mp3", as_attachment=True)
    except:
        print("error finding file")


@app.route("/convert", methods = ["POST"])
def another_page():
    print("Is converting")
    all_file_names = run(request.form['url'])
# Test url 1 "https://youtu.be/Yaelm87PTvg"
# Test url 2 "https://www.youtube.com/playlist?list=PLa9RjmTB4e_NQixvS1pFt-H_1hOUOOjXS"
    return all_file_names


if __name__ == '__main__':
    app.run()


