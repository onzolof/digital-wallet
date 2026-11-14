export type AmountParts = {
  currency: string
  sign: string
  amount: string
  negative: boolean
}

export function splitAmount(value: number, currency = 'CHF'): AmountParts {
  const negative = value < 0
  const amount = new Intl.NumberFormat('de-CH', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(value))
  return {
    currency,
    sign: negative ? '-' : '',
    amount,
    negative,
  }
}

export const formatMoney = (value: number, currency = 'CHF') => {
  const parts = splitAmount(value, currency)
  return `${parts.currency} ${parts.sign}${parts.amount}`
}

export const formatDate = (value: string) =>
  new Intl.DateTimeFormat('de-CH', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value))

export const isCredit = (indicator: string) =>
  indicator.toUpperCase() === 'CREDIT'
