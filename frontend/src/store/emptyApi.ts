import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/digital-wallet'

export const emptySplitApi = createApi({
  reducerPath: 'walletApi',
  baseQuery: fetchBaseQuery({ baseUrl: apiBaseUrl }),
  endpoints: () => ({}),
})
