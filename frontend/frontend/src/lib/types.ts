export type LoginResponse = {
  accessToken: string;
  tokenType?: string;
};

export type Holding = {
  id: number;
  symbol: string;
  name: string;
  quantity: number;
};

export type PriceMap = Record<string, number>;
