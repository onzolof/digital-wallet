import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { AccountCard } from '@/components/AccountCard.tsx'
import { Breadcrumb } from '@/components/Breadcrumb.tsx'
import { DashboardHeader } from '@/components/DashboardHeader.tsx'
import { Money } from '@/components/Money.tsx'
import { TransactionChart } from '@/components/TransactionChart.tsx'
import { TransactionTable } from '@/components/TransactionTable.tsx'
import {
  useGetAccountOverviewQuery,
  useGetAccountQuery,
} from '@/store/walletApi'

export function AccountDashboard() {
  const overviewQuery = useGetAccountOverviewQuery()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const accountQuery = useGetAccountQuery(
    { accountId: selectedId ?? '' },
    { skip: selectedId === null },
  )
  const overview = overviewQuery.data
  const selected = accountQuery.data
  const listed = overview?.accounts.find(
    (account) => account.accountId === selectedId,
  )
  const detail = selected ?? listed

  return (
    <main className="min-h-screen bg-wallet-page text-foreground">
      <div className="mx-auto w-full max-w-[1120px] px-6 pb-16 pt-7">
        <Breadcrumb
          accountId={selectedId}
          onAccounts={() => setSelectedId(null)}
        />
        {overviewQuery.isLoading ? (
          <p className="mt-7 rounded-[20px] bg-card px-8 py-7 text-muted-foreground">
            Loading your accounts…
          </p>
        ) : overviewQuery.isError || !overview ? (
          <p className="mt-7 rounded-[20px] bg-card px-8 py-7 text-muted-foreground">
            Accounts could not be loaded.
          </p>
        ) : !selectedId ? (
          <>
            <DashboardHeader
              username={overview.username}
              totalBalanceChf={overview.totalBalanceChf}
            />
            <section className="mt-9">
              <div>
                <h2 className="m-0 flex items-center gap-2.5 text-[1.75rem] font-semibold tracking-tight text-foreground">
                  Your accounts{' '}
                  <Badge variant="secondary" className="min-w-7 justify-center">
                    {String(overview.accounts.length).padStart(2, '0')}
                  </Badge>
                </h2>
              </div>
              <div className="mt-[18px] grid grid-cols-1 gap-4 md:grid-cols-3">
                {overview.accounts.map((account) => (
                  <AccountCard
                    key={account.accountId}
                    account={account}
                    onSelect={setSelectedId}
                  />
                ))}
              </div>
            </section>
          </>
        ) : (
          <>
            <section>
              <button
                className="mb-[18px] inline-flex cursor-pointer items-center gap-2 border-0 bg-transparent p-0 text-muted-foreground [&_svg]:size-[18px]"
                type="button"
                onClick={() => setSelectedId(null)}
              >
                <ArrowLeft />
                All accounts
              </button>
              <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
                <div>
                  <h1 className="m-0 text-4xl font-semibold leading-tight tracking-tight text-foreground">
                    {detail?.accountCategory ?? 'Account'}
                  </h1>
                  <p className="mt-2 text-muted-foreground">
                    {selected?.accountType ?? ''}
                  </p>
                </div>
                <div className="text-left md:text-right">
                  <span className="block text-[13px] text-muted-foreground">
                    Current balance
                  </span>
                  {detail ? (
                    <Money
                      value={detail.totalBalance}
                      currency={detail.currency}
                      className="text-[2rem] font-semibold tracking-tight"
                    />
                  ) : (
                    <strong className="text-[2rem] tracking-tight">—</strong>
                  )}
                </div>
              </div>
            </section>
            {accountQuery.isLoading ? (
              <p className="mt-7 rounded-[20px] bg-card px-8 py-7 text-muted-foreground">
                Loading transactions…
              </p>
            ) : accountQuery.isError || !selected ? (
              <p className="mt-7 rounded-[20px] bg-card px-8 py-7 text-muted-foreground">
                This account could not be loaded.
              </p>
            ) : (
              <>
                <TransactionChart
                  transactions={selected.transactions}
                  currency={detail?.currency}
                />
                <TransactionTable
                  key={selected.accountId}
                  transactions={selected.transactions}
                />
              </>
            )}
          </>
        )}
      </div>
    </main>
  )
}
