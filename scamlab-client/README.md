# scamlab

A new Flutter project.

## Cloudflared running
One has to inject the APIs URI in the launch.json
with --dart-define

```
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Debug Flutter",
            "request": "launch",
            "type": "dart",
            "args": [
                "--dart-define=API_URL=https://bi-aerial-k-reload.trycloudflare.com",
                "--dart-define=WS_URL=ws://bi-aerial-k-reload.trycloudflare.com"
            ]
        }
    ]
}
```
That also means disabling, on chrome at least, CORS security features, for the WebApp to run on Chrome. 
```
--web-browser-flag=--disable-web-security
```