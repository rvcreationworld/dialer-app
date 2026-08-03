from fastapi import FastAPI, UploadFile, File
import uvicorn
from faster_whisper import WhisperModel
import requests
import json
import os
import shutil

app = FastAPI()

# Load faster-whisper model
print("Loading Whisper model...")
model_size = "tiny"
model = WhisperModel(model_size, device="cpu", compute_type="int8")
print("Whisper model loaded!")

@app.post("/summarize")
async def summarize(audio: UploadFile = File(...)):
    # 1. Save uploaded file
    file_location = f"temp_{audio.filename}"
    with open(file_location, "wb+") as file_object:
        shutil.copyfileobj(audio.file, file_object)

    try:
        # 2. Transcribe Audio
        print(f"Transcribing {file_location}...")
        segments, info = model.transcribe(file_location, beam_size=5)
        transcription = " ".join([segment.text for segment in segments])
        print(f"Transcription complete: {transcription}")

        # 3. Summarize using Ollama
        prompt = f"You are an expert telecaller assistant. Read the following call transcription and provide a strictly 3-bullet-point summary of the customer's intent and any follow-up actions required. Be extremely concise.\n\nTranscription:\n{transcription}"
        
        print("Sending to Ollama (llama3.2:1b)...")
        response = requests.post("http://localhost:11434/api/generate", json={
            "model": "llama3.2:1b",
            "prompt": prompt,
            "stream": False
        })
        
        if response.status_code == 200:
            summary = response.json().get("response", "Failed to get response from Ollama")
            return {"summary": summary}
        else:
            return {"summary": f"Ollama Error: {response.text}"}

    except Exception as e:
        return {"summary": f"Server Error: {str(e)}"}
    finally:
        # Clean up audio file
        if os.path.exists(file_location):
            os.remove(file_location)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5050)
