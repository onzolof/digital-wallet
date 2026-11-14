import type { LucideIcon } from 'lucide-react'
import { ChevronRight, Coins, CreditCard, PiggyBank, Plane } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Money } from '@/components/dashboard/Money'
import { formatMoney } from '@/components/dashboard/utils'
import type { MinimalAccount } from '@/store/walletApi'

function accountIcon(category: string): LucideIcon {
  const normalized = category.toLowerCase()
  if (normalized.includes('saving') || normalized === 'svgs') return PiggyBank
  if (normalized.includes('salary')) return Coins
  if (normalized.includes('retirement')) return CreditCard
  if (normalized.includes('travel')) return Plane
  return CreditCard
}

type AccountCardProps = {
  account: MinimalAccount
  onSelect: (accountId: string) => void
}

export function AccountCard({ account, onSelect }: AccountCardProps) {
  const Icon = accountIcon(account.accountCategory)

  return (
    <button
      className="cursor-pointer border-0 bg-transparent p-0 text-left"
      type="button"
      onClick={() => onSelect(account.accountId)}
      aria-label={`Open ${account.accountCategory}, account ${account.accountId}, balance ${formatMoney(account.totalBalance, account.currency)}`}
    >
      <Card className="h-full transition duration-150 ease-out hover:-translate-y-0.5 hover:shadow-md">
        <CardContent>
          <div className="flex items-center justify-between gap-4">
            <span className="inline-flex size-[42px] items-center justify-center rounded-xl bg-income-subtle text-income [&_svg]:size-[18px]">
              <Icon />
            </span>
            <ChevronRight className="size-[18px] text-muted-foreground" />
          </div>
          <div className="mt-5 mb-3 text-lg font-semibold">
            {account.accountCategory}
          </div>
          <div className="mb-3 break-all text-[13px] text-muted-foreground">
            {account.accountId}
          </div>
          <Money
            value={account.totalBalance}
            currency={account.currency}
            className="mt-5 text-2xl font-semibold tracking-tight"
          />
        </CardContent>
      </Card>
    </button>
  )
}
