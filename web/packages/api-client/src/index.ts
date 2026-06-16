export type FetchLike = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export type HelloResponse = {
    message: string;
    serverTime: string;
};

export const apiBaseUrl = "";

export async function hello(fetcher: FetchLike = fetch): Promise<HelloResponse> {
    const response = await fetcher(`${apiBaseUrl}/api/hello`);

    if (!response.ok) {
        throw new Error(`GET /api/hello failed with ${response.status}`);
    }

    return response.json() as Promise<HelloResponse>;
}
