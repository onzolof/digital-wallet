import { emptySplitApi as api } from './emptyApi'
const injectedRtkApi = api.injectEndpoints({
  endpoints: (build) => ({
    getAccountOverview: build.query<
      GetAccountOverviewApiResponse,
      GetAccountOverviewApiArg
    >({
      query: () => ({ url: `/accounts` }),
    }),
    getAccount: build.query<GetAccountApiResponse, GetAccountApiArg>({
      query: (queryArg) => ({ url: `/accounts/${queryArg.accountId}` }),
    }),
  }),
  overrideExisting: false,
})
export { injectedRtkApi as walletApi }
export type GetAccountOverviewApiResponse =
  /** status 200 Account overview */ AccountOverview
export type GetAccountOverviewApiArg = void
export type GetAccountApiResponse = /** status 200 The account */ Account
export type GetAccountApiArg = {
  accountId: string
}
export type MinimalAccount = {
  userId: string
  username: string
  accountId: string
  currency: string
  accountCategory: string
  /** Account balance in the account currency, as a decimal. */
  totalBalance: number
  /** Account balance in CHF, as a decimal. */
  totalBalanceChf: number
}
export type AccountOverview = {
  userId: string
  username: string
  /** Total balance in CHF, as a decimal. */
  totalBalanceChf: number
  accounts: MinimalAccount[]
}
export type Transaction = {
  accountId: string
  transactionId: string
  amount: number
  amountRounded: number
  currency: string
  creditDebitIndicator: string
  status: string
  bookingDateTime: string
  transactionInformation: string
}
export type Account = {
  userId: string
  username: string
  accountId: string
  currency: string
  accountType: string
  accountCategory: string
  totalBalance: number
  totalBalanceChf: number
  transactions: Transaction[]
}
export const { useGetAccountOverviewQuery, useGetAccountQuery } = injectedRtkApi
