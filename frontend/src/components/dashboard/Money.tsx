import { cn } from '@/lib/utils'
import { splitAmount } from '@/components/dashboard/utils'

type MoneyProps = {
  value: number
  currency?: string
  className?: string
  amountClassName?: string
  variant?: 'default' | 'onDark'
}

export function Money({
  value,
  currency = 'CHF',
  className,
  amountClassName,
  variant = 'default',
}: MoneyProps) {
  const parts = splitAmount(value, currency)
  const currencyClass =
    variant === 'onDark' ? 'text-wallet-hero-muted' : 'text-foreground'
  const positiveAmountClass =
    variant === 'onDark' ? 'text-wallet-hero-muted' : 'text-foreground'
  const negativeAmountClass =
    variant === 'onDark' ? 'text-destructive/60' : 'text-destructive/75'

  return (
    <span className={className}>
      <span className={currencyClass}>{parts.currency} </span>
      <span
        className={cn(
          parts.negative ? negativeAmountClass : positiveAmountClass,
          amountClassName,
        )}
      >
        {parts.sign}
        {parts.amount}
      </span>
    </span>
  )
}