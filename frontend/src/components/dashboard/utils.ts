export const formatMoney = (amount: number, currency = 'CHF') =>
  new Intl.NumberFormat('de-CH', { style: 'currency', currency }).format(amount)

export const formatDate = (value: string) =>
  new Intl.DateTimeFormat('de-CH', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value))

export const isCredit = (indicator: string) =>
  indicator.toUpperCase() === 'CREDIT'

export const balanceTone = (amount: number) =>
  amount < 0 ? 'text-red-600' : 'text-[#17251f]'
