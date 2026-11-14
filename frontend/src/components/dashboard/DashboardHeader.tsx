import { LineChart } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Money } from '@/components/dashboard/Money'

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
          <h1 className="m-0 text-4xl font-semibold leading-tight tracking-tight text-foreground">
            Good morning, {username}
          </h1>
          <p className="mt-2 text-muted-foreground">
            Here&apos;s what&apos;s happening with your money.
          </p>
        </div>
      </section>
      <Card className="mt-7 border-0 bg-wallet-hero text-wallet-hero-foreground shadow-none">
        <CardContent className="p-7">
          <div className="flex items-stretch gap-4">
            <span className="flex w-14 shrink-0 items-center justify-center rounded-xl bg-wallet-hero-foreground/10 text-wallet-hero-foreground [&_svg]:size-6">
              <LineChart aria-hidden />
            </span>
            <div className="min-w-0 flex-1">
              <div className="text-sm text-wallet-hero-muted">
                Total balance
              </div>
              <Money
                value={totalBalanceChf}
                currency="CHF"
                variant="onDark"
                className="mt-4 text-[2.625rem] font-semibold leading-none tracking-tight"
              />
            </div>
          </div>
        </CardContent>
      </Card>
    </>
  )
}
