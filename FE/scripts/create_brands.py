import requests
import time
import json

# ==============================
# CONFIG
# ==============================

BASE_URL = "http://localhost:2000"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
REFRESH_URL = f"{BASE_URL}/api/auth/refresh"
BRAND_API = f"{BASE_URL}/api/admin/brands"

EMAIL = "ad@gmail.com"
PASSWORD = "123456"

BRAND_FILE = "brands.json"

session = requests.Session()
session.headers.update({
    "Content-Type": "application/json"
})


# ==============================
# AUTH
# ==============================

def login():
    payload = {
        "email": EMAIL,
        "password": PASSWORD
    }

    response = session.post(LOGIN_URL, json=payload)

    if response.status_code == 200:
        print("✅ Login successful")
    else:
        print("❌ Login failed:", response.text)
        exit()


def refresh_token():
    response = session.post(REFRESH_URL)

    if response.status_code == 200:
        print("🔄 Token refreshed")
    else:
        print("❌ Refresh failed:", response.text)
        exit()


def safe_post(url, payload):
    response = session.post(url, json=payload)

    if response.status_code == 401:
        print("⚠️ Token expired → Refreshing...")
        refresh_token()
        response = session.post(url, json=payload)

    return response


def safe_get(url):
    response = session.get(url)

    if response.status_code == 401:
        print("⚠️ Token expired → Refreshing...")
        refresh_token()
        response = session.get(url)

    return response


# ==============================
# GET EXISTING BRANDS
# ==============================

def get_existing_brands():
    response = safe_get(BRAND_API)

    if response.status_code != 200:
        return []

    return response.json()


def brand_exists(name, existing_brands):
    return any(b["name"].lower() == name.lower() for b in existing_brands)


# ==============================
# CREATE BRAND
# ==============================

def create_brand(name, logo_url):
    payload = {
        "name": name,
        "logoUrl": logo_url
    }

    response = safe_post(BRAND_API, payload)

    if response.status_code not in [200, 201]:
        print(f"❌ Failed create brand: {name}")
        print(response.text)
        return None

    print(f"✅ Created brand: {name}")
    return response.json().get("id")


# ==============================
# MAIN
# ==============================

def main():

    login()

    with open(BRAND_FILE, "r", encoding="utf-8") as f:
        brands = json.load(f)

    existing_brands = get_existing_brands()

    for brand in brands:

        name = brand["name"]
        logo = brand.get("logoUrl")

        if brand_exists(name, existing_brands):
            print(f"⏩ Skip existing brand: {name}")
            continue

        create_brand(name, logo)

        time.sleep(0.1)

    print("\n🎉 DONE! Brands seeded successfully.")


if __name__ == "__main__":
    main()