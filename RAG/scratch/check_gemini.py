import os
from google import genai
from google.genai import types
from dotenv import load_dotenv

def test_gemini_connection():
    # Load environment variables from .env if it exists
    load_dotenv()
    
    api_key = os.getenv("GOOGLE_AI_API_KEY")
    if not api_key:
        print("❌ Error: GOOGLE_AI_API_KEY not found in environment variables.")
        return

    print(f"Connecting to Gemini with API Key: {api_key[:5]}...{api_key[-5:]}")
    
    try:
        client = genai.Client(api_key=api_key)
        
        # Test models list to verify connectivity
        print("Listing models...")
        models = client.models.list()
        print("✅ Connection successful! Available models:")
        for model in models:
            if "gemini" in model.name:
                print(f" - {model.name}")
        
        # Test a simple generation
        print("\nTesting simple generation (gemini-2.0-flash)...")
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents="Say 'Hello, Gemini connection is working!' if you can read this."
        )
        
        if response and response.text:
            print(f"✅ Generation successful! Response: {response.text}")
        else:
            print("❌ Generation completed but response text is empty.")
            
    except Exception as e:
        print(f"❌ Gemini connection failed: {str(e)}")

if __name__ == "__main__":
    test_gemini_connection()
