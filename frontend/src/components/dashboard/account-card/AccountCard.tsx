import { ChevronRight, CreditCard, PiggyBank, Plane } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { balanceTone, formatMoney } from '@/components/dashboard/utils'
import { cn } from '@/lib/utils'
import type { MinimalAccount } from '@/store/walletApi'

const iconTones = [
  'bg-[#e7f3ec] text-[#1f7a4d]',
  'bg-[#fff4d8] text-[#9a6700]',
  'bg-[#e7f0ff] text-[#2457c5]',
]

type AccountCardProps = {
  account: MinimalAccount
  index: number
  onSelect: (accountId: string) => void
}

export function AccountCard({ account, index, onSelect }: AccountCardProps) {
  return (
    <button
      className="cursor-pointer border-0 bg-transparent p-0 text-left"
      type="button"
      onClick={() => onSelect(account.accountId)}
      aria-label={`Open ${account.accountCategory}, account ${account.accountId}, balance ${formatMoney(account.totalBalance, account.currency)}`}
    >
      <Card className="h-full transition duration-150 ease-out hover:-translate-y-0.5 hover:shadow-[0_12px_30px_rgba(23,37,31,0.08)]">
        <CardContent>
          <div className="flex items-center justify-between gap-4">
            <span
              className={cn(
                'inline-flex size-[42px] items-center justify-center rounded-xl [&_svg]:size-[18px]',
                iconTones[index % iconTones.length],
              )}
            >
              {account.accountCategory === 'Savings' ? (
                <PiggyBank />
              ) : account.accountCategory === 'Travel' ? (
                <Plane />
              ) : (
                <CreditCard />
              )}
            </span>
            <ChevronRight className="size-[18px] text-[#98a2b3]" />
          </div>
          <div className="mt-[22px] text-lg font-semibold">
            {account.accountCategory}
          </div>
          <div className="mt-1 break-all text-[13px] text-[#667085]">
            {account.accountId}
          </div>
          <div
            className={cn(
              'mt-[22px] text-2xl font-semibold tracking-tight',
              balanceTone(account.totalBalance),
            )}
          >
            {formatMoney(account.totalBalance, account.currency)}
          </div>
        </CardContent>
      </Card>
    </button>
  )
}
