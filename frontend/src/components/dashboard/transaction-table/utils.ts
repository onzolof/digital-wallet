import type { Transaction } from '@/store/walletApi'

export type SortKey = 'bookingDateTime' | 'transactionInformation' | 'amount'
export type TransactionSort = { key: SortKey; direction: 'asc' | 'desc' }

export function toggleSort(
  current: TransactionSort,
  key: SortKey,
): TransactionSort {
  return {
    key,
    direction:
      current.key === key && current.direction === 'desc' ? 'asc' : 'desc',
  }
}

export function visibleTransactions(
  transactions: Transaction[],
  query: string,
  sort: TransactionSort,
) {
  return transactions
    .filter((item) =>
      item.transactionInformation.toLowerCase().includes(query.toLowerCase()),
    )
    .sort((left, right) => {
      const comparison =
        sort.key === 'amount'
          ? Number(left.amount) - Number(right.amount)
          : sort.key === 'bookingDateTime'
            ? new Date(left.bookingDateTime).getTime() -
              new Date(right.bookingDateTime).getTime()
            : left.transactionInformation.localeCompare(
                right.transactionInformation,
              )
      return sort.direction === 'asc' ? comparison : -comparison
    })
}
