import { fireEvent, render, screen } from '@testing-library/react'
import { expect, test, vi } from 'vitest'
import { AccountDashboard } from '@/components/account-dashboard'

const { overview, savingsAccount } = vi.hoisted(() => {
  const savingsAccount = {
    userId: 'user-001',
    username: 'Jamie Davis',
    accountId: 'NW-1093',
    currency: 'CHF',
    accountType: 'Personal savings',
    accountCategory: 'Savings',
    totalBalance: 12350,
    totalBalanceChf: 12350,
    transactions: [
      {
        accountId: 'NW-1093',
        transactionId: 'TX-2003',
        amount: 125,
        amountRounded: 125,
        currency: 'CHF',
        creditDebitIndicator: 'Credit',
        status: 'Booked',
        bookingDateTime: '2025-05-23T09:00:00',
        transactionInformation: 'Monthly savings',
      },
      {
        accountId: 'NW-1093',
        transactionId: 'TX-2002',
        amount: 35,
        amountRounded: 35,
        currency: 'CHF',
        creditDebitIndicator: 'Debit',
        status: 'Booked',
        bookingDateTime: '2025-05-21T12:00:00',
        transactionInformation: 'Transfer to current',
      },
      {
        accountId: 'NW-1093',
        transactionId: 'TX-2001',
        amount: 42.18,
        amountRounded: 42,
        currency: 'CHF',
        creditDebitIndicator: 'Credit',
        status: 'Booked',
        bookingDateTime: '2025-05-15T12:00:00',
        transactionInformation: 'Interest earned',
      },
    ],
  }
  const overview = {
    userId: 'user-001',
    username: 'Jamie Davis',
    totalBalanceChf: 19357.17,
    accounts: [
      {
        userId: 'user-001',
        username: 'Jamie Davis',
        accountId: 'NW-4829',
        currency: 'GBP',
        accountCategory: 'Current',
        totalBalance: 4826.42,
        totalBalanceChf: 4826.42,
      },
      {
        userId: savingsAccount.userId,
        username: savingsAccount.username,
        accountId: savingsAccount.accountId,
        currency: savingsAccount.currency,
        accountCategory: savingsAccount.accountCategory,
        totalBalance: savingsAccount.totalBalance,
        totalBalanceChf: savingsAccount.totalBalanceChf,
      },
    ],
  }
  return { overview, savingsAccount }
})

vi.mock('@/store/walletApi', () => ({
  useGetAccountOverviewQuery: () => ({
    data: overview,
    isLoading: false,
    isError: false,
  }),
  useGetAccountQuery: (
    argument: { accountId: string },
    options?: { skip?: boolean },
  ) => ({
    data:
      options?.skip || argument.accountId !== savingsAccount.accountId
        ? undefined
        : savingsAccount,
    isLoading: false,
    isError: false,
  }),
}))

test('opens an account and filters its transactions', () => {
  render(<AccountDashboard />)

  expect(
    screen.getByRole('heading', { name: 'Good morning, Jamie Davis' }),
  ).toBeVisible()

  fireEvent.click(screen.getByRole('button', { name: /Open Savings/ }))

  expect(screen.getByRole('heading', { name: 'Savings' })).toBeVisible()
  expect(screen.getByText('Interest earned')).toBeVisible()

  fireEvent.change(
    screen.getByRole('textbox', { name: 'Search transactions' }),
    {
      target: { value: 'transfer' },
    },
  )

  expect(screen.getByText('Transfer to current')).toBeVisible()
  expect(screen.queryByText('Interest earned')).not.toBeInTheDocument()
})
