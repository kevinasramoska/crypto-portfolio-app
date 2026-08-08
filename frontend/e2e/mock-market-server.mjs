import http from "node:http";
import { URL } from "node:url";

const PORT = 9099;

const PRICES = {
  bitcoin: 60000,
  ethereum: 3000,
  solana: 150,
  chainlink: 18,
  dogecoin: 0.2,
};

const server = http.createServer((req, res) => {
  if (!req.url) {
    res.writeHead(400, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Missing URL" }));
    return;
  }

  const url = new URL(req.url, `http://127.0.0.1:${PORT}`);

  if (req.method === "GET" && url.pathname === "/api/v3/simple/price") {
    const ids = url.searchParams.get("ids")?.split(",").filter(Boolean) ?? [];
    const body = Object.fromEntries(
      ids.map(id => [id, { usd: PRICES[id] ?? 1 }])
    );

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify(body));
    return;
  }

  res.writeHead(404, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ error: "Not found" }));
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`Mock market server listening on http://127.0.0.1:${PORT}`);
});
