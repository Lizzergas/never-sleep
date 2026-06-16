import { useState } from "react";
import { apiBaseUrl, hello, type HelloResponse } from "@myapptemplate/api-client";
import "./styles.css";

type ApiStatus =
    | {
          kind: "idle";
      }
    | {
          kind: "loading";
      }
    | {
          kind: "success";
          data: HelloResponse;
      }
    | {
          kind: "error";
          message: string;
      };

export function getApiStatusLabel(status: ApiStatus): string {
    switch (status.kind) {
        case "idle":
            return "API not checked yet";
        case "loading":
            return "Checking API";
        case "success":
            return status.data.message;
        case "error":
            return status.message;
    }
}

export function App() {
    const [status, setStatus] = useState<ApiStatus>({ kind: "idle" });

    async function checkApi() {
        setStatus({ kind: "loading" });
        try {
            const data = await hello();
            setStatus({ kind: "success", data });
        } catch (error) {
            setStatus({
                kind: "error",
                message: error instanceof Error ? error.message : "Unknown API error",
            });
        }
    }

    return (
        <main className="app-shell">
            <header className="topbar">
                <div>
                    <p className="eyebrow">Vite + React + TypeScript</p>
                    <h1>Admin workspace</h1>
                </div>
                <a href="http://localhost:5173">Landing</a>
            </header>

            <section className="dashboard" aria-labelledby="status-title">
                <div className="status-panel">
                    <p className="eyebrow">Ktor proxy</p>
                    <h2 id="status-title">API status</h2>
                    <p>{getApiStatusLabel(status)}</p>
                    {status.kind === "success" ? <time>{status.data.serverTime}</time> : null}
                    <button type="button" onClick={checkApi} disabled={status.kind === "loading"}>
                        {status.kind === "loading" ? "Checking" : "Check /api/hello"}
                    </button>
                </div>

                <div className="details-panel">
                    <dl>
                        <div>
                            <dt>Admin</dt>
                            <dd>http://localhost:5174</dd>
                        </div>
                        <div>
                            <dt>API base</dt>
                            <dd>{apiBaseUrl || "same origin"}</dd>
                        </div>
                        <div>
                            <dt>Proxy target</dt>
                            <dd>http://localhost:8080</dd>
                        </div>
                    </dl>
                </div>
            </section>
        </main>
    );
}
