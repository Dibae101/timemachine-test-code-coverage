from fastapi import FastAPI
from datetime import datetime
import httpx
import asyncio

app = FastAPI()
rates = {}
frankfurter = "https://api.frankfurter.app/latest"
host = "0.0.0.0"
port = 8000

async def load():
    global rates
    async with httpx.AsyncClient() as client:
        response = await client.get(frankfurter)
        data = response.json()
        api_rates = data["rates"]
        all_currencies = list(api_rates.keys()) + ["EUR"]

        # rates for other currencies
        for base_currency in all_currencies:
            base_lower = base_currency.lower()
            rates[base_lower] = {}

            for target_currency in all_currencies:
                target_lower = target_currency.lower()

                if base_lower == target_lower:
                    rates[base_lower][target_lower] = 1.0
                elif base_currency == "EUR":
                    rates[base_lower][target_lower] = api_rates.get(
                        target_currency, 1.0
                    )
                elif target_currency == "EUR":
                    rates[base_lower][target_lower] = 1.0 / api_rates.get(
                        base_currency, 1.0
                    )
                else:
                    eur_to_base = api_rates.get(base_currency, 1.0)
                    eur_to_target = api_rates.get(target_currency, 1.0)
                    rates[base_lower][target_lower] = (
                        eur_to_target / eur_to_base if eur_to_base != 0 else 0
                    )

        print(f"Rates loaded for {len(rates)} currencies: {list(rates.keys())}")


async def update():
    while True:
        await asyncio.sleep(86400)
        await load()


@app.on_event("startup") # deprecated but who cares
async def start():
    await load()
    asyncio.create_task(update())


@app.get("/v1/currencies/{currency}.json")
async def get(currency: str):
    return {
        "date": datetime.now().date().isoformat(),
        currency: rates.get(currency.lower(), {}),
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=host, port=port)
