import { Card, CardContent } from '@/components/ui/card'
import { formatMoney } from '@/components/dashboard/utils'

type DashboardHeaderProps = {
  username: string
  totalBalanceChf: number
}

export function DashboardHeader({
  username,
  totalBalanceChf,
}: DashboardHeaderProps) {
  return (
    <>
      <section>
        <div>
          <p className="mb-2 text-xs font-semibold tracking-[0.14em] text-gray-500">
            YOUR MONEY, AT A GLANCE
          </p>
          <h1 className="m-0 text-4xl font-semibold leading-tight tracking-tight text-[#17251f]">
            Good morning, {username}
          </h1>
          <p className="mt-2 text-[#667085]">
            Here&apos;s what&apos;s happening with your money.
          </p>
        </div>
      </section>
      <Card className="mt-7 border-0 bg-[#16352b] text-white shadow-none">
        <CardContent className="p-7">
          <div className="text-sm text-[#d5e4dc]">Total balance</div>
          <p
            className={
              totalBalanceChf < 0
                ? 'mt-2 text-[2.625rem] font-semibold leading-none tracking-tight text-red-400'
                : 'mt-2 text-[2.625rem] font-semibold leading-none tracking-tight text-white'
            }
          >
            {formatMoney(totalBalanceChf, 'CHF')}
          </p>
        </CardContent>
      </Card>
    </>
  )
}
