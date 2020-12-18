import sys
import pafy
import os
from moviepy.editor import *

all_file_names = []


def download_and_convert_file(video_file, counter=0):
    # checks if file exists already
    if os.path.isfile(video_file.title + ".mp3"):
        print("file already exists")
        return False

    # this section downloads the file
    try:
        video_file.getbest().download()
    except:
        if counter < 3:  # tries to download the file 3 times
            download_and_convert_file(video_file, counter + 1)
        print("Content could not be downloaded")
        return False

    # convert to mp3
    try:
        file = VideoFileClip(video_file.title + ".mp4")
        file.audio.write_audiofile(convert_name_to_query(video_file.title) + ".mp3")
        all_file_names.append(convert_name_to_query(video_file.title))
    except FileExistsError:
        print("file already exists")

    # this section removes the mp4 file
    try:
        os.remove(video_file.title+".mp4")
    except FileNotFoundError:
        print("file:" + video_file.title + ".mp4 not found")

    return True


def convert_name_to_query(name):
    letters = list(name)
    query = ""
    for letter in letters:
        if letter.isalpha() or letter.isdigit() or " " in letter:
            query += letter
    return "_".join(query.split())  # all spaces become underscores


def run(url):
    video = None  # list of the class of videos
    is_playlist = False
    is_valid_link = True
    if os.getcwd() != '/Users/petr-konstantin/Desktop/submitIBCS/Product/CS-IA_FULL-CODE/Serverside/Downloads':
        os.chdir(os.getcwd()+"/Downloads")

    try:
        video = pafy.new(url)
    except:
        is_valid_link = False
    if not is_valid_link:
        try:
            video = pafy.get_playlist(url)
            is_playlist = True
        except:
            print("Invalid Link")
            return "Invalid link"

    if not is_playlist:
        # make_and_enter_file(video.title)
        if download_and_convert_file(video):
            print("Video downloaded: " + video.title)

    else:
        # make_and_enter_file(video["title"])  # this is the playlist title
        for v in video["items"]:
            individual_vid = v["pafy"]
            if download_and_convert_file(individual_vid):
                print("Video downloaded: " + individual_vid.title)

    return "!!!".join(all_file_names)


