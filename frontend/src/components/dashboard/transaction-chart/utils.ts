import { isCredit } from '@/components/dashboard/utils'
import type { Transaction } from '@/store/walletApi'

export type CashFlowDay = {
  key: string
  day: string
  income: number
  expenses: number
  incomeAmount: number
  expenseAmount: number
}

export type CashFlow = {
  axisMax: number
  monthLabel: string
  days: CashFlowDay[]
}

export function cashFlow(transactions: Transaction[]): CashFlow {
  const buckets = new Map<
    string,
    { day: string; income: number; expenses: number; time: number }
  >()
  for (const transaction of transactions) {
    const date = new Date(transaction.bookingDateTime)
    if (Number.isNaN(date.getTime())) continue
    const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    const bucket = buckets.get(key) ?? {
      day: String(date.getDate()),
      income: 0,
      expenses: 0,
      time: new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
      ).getTime(),
    }
    const amount = Math.abs(Number(transaction.amount))
    if (isCredit(transaction.creditDebitIndicator)) bucket.income += amount
    else bucket.expenses += amount
    buckets.set(key, bucket)
  }

  const recent = [...buckets.values()]
    .sort((left, right) => left.time - right.time)
    .slice(-14)
  const peak = recent.reduce(
    (max, item) => Math.max(max, item.income, item.expenses),
    0,
  )
  const magnitude = peak <= 0 ? 1 : 10 ** Math.floor(Math.log10(peak))
  const axisMax = peak <= 0 ? 1000 : magnitude * Math.ceil(peak / magnitude)
  const latest = recent.at(-1)
  return {
    axisMax,
    monthLabel: latest
      ? new Intl.DateTimeFormat('de-CH', { month: 'long', year: 'numeric' })
          .format(new Date(latest.time))
          .toUpperCase()
      : 'NO ACTIVITY',
    days: recent.map((item) => ({
      key: String(item.time),
      day: item.day,
      income: (item.income / axisMax) * 100,
      expenses: (item.expenses / axisMax) * 100,
      incomeAmount: item.income,
      expenseAmount: item.expenses,
    })),
  }
}
