import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class CalculatorServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", CalculatorServer::servePage);
        server.setExecutor(null);
        server.start();
        System.out.println("Calculator running at http://localhost:" + PORT);
    }

    private static void servePage(HttpExchange exchange) throws IOException {
        byte[] page = HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, page.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(page);
        }
    }

    private static final String HTML = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Java Calculator</title>
              <style>
                :root { --ink: #17212b; --muted: #72808d; --paper: #f4f0e9; --panel: #fffdf9; --accent: #e56b4f; --line: #e8e0d6; }
                * { box-sizing: border-box; }
                body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 24px; color: var(--ink); background: radial-gradient(circle at 12% 12%, #f4c7a8 0 10%, transparent 28%), linear-gradient(135deg, #e9eef0, var(--paper)); font-family: Georgia, 'Times New Roman', serif; }
                main { width: min(100%, 390px); }
                .eyebrow { margin: 0 0 8px 4px; color: var(--accent); font: 700 12px/1.2 Arial, sans-serif; letter-spacing: 2px; text-transform: uppercase; }
                h1 { margin: 0 0 18px 4px; font-size: clamp(32px, 8vw, 46px); line-height: .95; }
                .calculator { padding: 20px; border: 1px solid rgba(23, 33, 43, .08); border-radius: 8px; background: rgba(255, 253, 249, .88); box-shadow: 0 24px 60px rgba(23, 33, 43, .14); backdrop-filter: blur(10px); }
                .display { min-height: 118px; display: flex; flex-direction: column; justify-content: end; align-items: end; overflow: hidden; padding: 18px; margin-bottom: 16px; border-radius: 6px; background: var(--ink); color: white; }
                .expression { width: 100%; overflow: hidden; color: #aeb8bf; font: 14px/1.4 Arial, sans-serif; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
                .value { width: 100%; overflow: hidden; font-size: clamp(36px, 10vw, 52px); line-height: 1.1; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
                .keys { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
                button { min-height: 58px; border: 1px solid var(--line); border-radius: 6px; color: var(--ink); background: #f8f5ef; font: 600 20px Georgia, serif; cursor: pointer; transition: transform .12s ease, background .12s ease; }
                button:hover { background: #eee7dc; }
                button:active { transform: translateY(2px); }
                .operator { color: var(--accent); background: #fff4ed; }
                .equals { color: white; border-color: var(--accent); background: var(--accent); }
                .wide { grid-column: span 2; }
                footer { margin: 16px 4px 0; color: var(--muted); font: 12px Arial, sans-serif; text-align: center; }
              </style>
            </head>
            <body>
              <main>
                <p class="eyebrow">Java powered</p>
                <h1>Simple sums.</h1>
                <section class="calculator" aria-label="Calculator">
                  <div class="display" aria-live="polite">
                    <div class="expression" id="expression">&nbsp;</div>
                    <div class="value" id="value">0</div>
                  </div>
                  <div class="keys">
                    <button class="operator" data-action="clear">AC</button>
                    <button class="operator" data-action="sign">+/-</button>
                    <button class="operator" data-action="percent">%</button>
                    <button class="operator" data-action="operator" data-value="/">÷</button>
                    <button data-value="7">7</button><button data-value="8">8</button><button data-value="9">9</button>
                    <button class="operator" data-action="operator" data-value="*">×</button>
                    <button data-value="4">4</button><button data-value="5">5</button><button data-value="6">6</button>
                    <button class="operator" data-action="operator" data-value="-">−</button>
                    <button data-value="1">1</button><button data-value="2">2</button><button data-value="3">3</button>
                    <button class="operator" data-action="operator" data-value="+">+</button>
                    <button class="wide" data-value="0">0</button><button data-action="decimal">.</button>
                    <button class="equals" data-action="equals">=</button>
                  </div>
                </section>
                <footer>Use your keyboard, too.</footer>
              </main>
              <script>
                let current = '0', stored = null, pending = null, reset = false;
                const value = document.querySelector('#value');
                const expression = document.querySelector('#expression');
                const render = () => { value.textContent = current; expression.textContent = stored !== null && pending ? stored + ' ' + pending : '\\u00a0'; };
                const calculate = (a, op, b) => ({ '+': a + b, '-': a - b, '*': a * b, '/': b === 0 ? NaN : a / b }[op]);
                function input(digit) { if (reset || current === 'Error') { current = digit; reset = false; } else current = current === '0' ? digit : current + digit; render(); }
                function operator(op) { const number = Number(current); if (stored !== null && pending && !reset) stored = calculate(stored, pending, number); else stored = number; pending = op; reset = true; render(); }
                function equals() { if (stored === null || !pending) return; const result = calculate(stored, pending, Number(current)); current = Number.isFinite(result) ? String(Number(result.toFixed(10))) : 'Error'; stored = null; pending = null; reset = true; render(); }
                function clear() { current = '0'; stored = null; pending = null; reset = false; render(); }
                document.querySelectorAll('button').forEach(button => button.addEventListener('click', () => {
                  const action = button.dataset.action, digit = button.dataset.value;
                  if (digit && !action) input(digit);
                  else if (action === 'operator') operator(digit);
                  else if (action === 'equals') equals();
                  else if (action === 'clear') clear();
                  else if (action === 'decimal' && !current.includes('.')) input('.');
                  else if (action === 'sign') current = String(Number(current) * -1), render();
                  else if (action === 'percent') current = String(Number(current) / 100), render();
                }));
                document.addEventListener('keydown', event => { const key = event.key; if ('0123456789.'.includes(key)) input(key); else if ('+-*/'.includes(key)) operator(key); else if (key === 'Enter' || key === '=') equals(); else if (key === 'Escape') clear(); else if (key === '%') current = String(Number(current) / 100), render(); });
              </script>
            </body>
            </html>
            """;
}