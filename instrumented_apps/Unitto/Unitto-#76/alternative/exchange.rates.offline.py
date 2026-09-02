from fastapi import FastAPI
from datetime import datetime
import asyncio
import random

app = FastAPI()
rates = {}


async def load():
    global rates
    rates["eur"] = {
        "usd": round(1.08 + random.uniform(-0.05, 0.05), 4),
        "gbp": round(0.86 + random.uniform(-0.03, 0.03), 4),
        "jpy": round(161.5 + random.uniform(-2, 2), 2),
        "aud": round(1.66 + random.uniform(-0.05, 0.05), 4),
        "cad": round(1.47 + random.uniform(-0.05, 0.05), 4),
        "chf": round(0.95 + random.uniform(-0.02, 0.02), 4),
        "cny": round(7.85 + random.uniform(-0.1, 0.1), 4),
    }

    if "usd" in rates["eur"]:
        eur_usd = rates["eur"]["usd"]
        usd_rates = {}
        for currency, rate in rates["eur"].items():
            if currency != "usd":
                usd_rates[currency] = round(rate / eur_usd, 4)
        usd_rates["eur"] = round(1 / eur_usd, 4)
        rates["usd"] = usd_rates

    print(f"Dummy rates created for: {list(rates.keys())}")


async def update():
    while True:
        await asyncio.sleep(86400)  # 24 hours
        await load()


@app.on_event("startup")
async def start():
    await load()
    asyncio.create_task(update())


@app.get("/v1/currencies/{currency}.json")
async def get(currency: str):
    currency = currency.lower()
    if currency not in rates:
        return {"error": f"Currency {currency} not available. Try 'eur' or 'usd'."}

    return {"date": datetime.now().date().isoformat(), currency: rates[currency]}


if __name__ == "__main__":
    import uvicorn

    print("DUMMY CURRENCY API SERVER")
    print("Access at: http://localhost:8000")
    print("Example: http://localhost:8000/v1/currencies/eur.json")
    uvicorn.run(app, host="0.0.0.0", port=8000)
