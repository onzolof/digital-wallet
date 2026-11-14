import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { AccountCard } from '@/components/dashboard/account-card/AccountCard'
import { Breadcrumb } from '@/components/dashboard/breadcrumb/Breadcrumb'
import { DashboardHeader } from '@/components/dashboard/dashboard-header/DashboardHeader'
import { TransactionChart } from '@/components/dashboard/transaction-chart/TransactionChart'
import { TransactionTable } from '@/components/dashboard/transaction-table/TransactionTable'
import { balanceTone, formatMoney } from '@/components/dashboard/utils'
import { cn } from '@/lib/utils'
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
    <main className="min-h-screen bg-[#f7f8fa] text-[#17251f]">
      <div className="mx-auto w-full max-w-[1120px] px-6 pb-16 pt-7">
        <Breadcrumb
          accountId={selectedId}
          onAccounts={() => setSelectedId(null)}
        />
        {overviewQuery.isLoading ? (
          <p className="mt-7 rounded-[20px] bg-white px-8 py-7 text-[#667085]">
            Loading your accounts…
          </p>
        ) : overviewQuery.isError || !overview ? (
          <p className="mt-7 rounded-[20px] bg-white px-8 py-7 text-[#667085]">
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
                <p className="mb-2 text-xs font-semibold tracking-[0.14em] text-gray-500">
                  CONNECTED BANKING
                </p>
                <h2 className="m-0 flex items-center gap-2.5 text-[1.75rem] font-semibold tracking-tight text-[#17251f]">
                  Your accounts{' '}
                  <Badge
                    variant="secondary"
                    className="min-w-7 justify-center bg-[#eef2ef] text-[#17251f]"
                  >
                    {String(overview.accounts.length).padStart(2, '0')}
                  </Badge>
                </h2>
              </div>
              <div className="mt-[18px] grid grid-cols-1 gap-4 md:grid-cols-3">
                {overview.accounts.map((account, index) => (
                  <AccountCard
                    key={account.accountId}
                    account={account}
                    index={index}
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
                className="mb-[18px] inline-flex cursor-pointer items-center gap-2 border-0 bg-transparent p-0 text-[#475467] [&_svg]:size-[18px]"
                type="button"
                onClick={() => setSelectedId(null)}
              >
                <ArrowLeft />
                All accounts
              </button>
              <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
                <div>
                  <p className="mb-2 text-xs font-semibold tracking-[0.14em] text-gray-500">
                    {(detail?.accountCategory ?? 'Account').toUpperCase()}{' '}
                    <span className="tracking-normal text-[#98a2b3]">
                      · {selectedId}
                    </span>
                  </p>
                  <h1 className="m-0 text-4xl font-semibold leading-tight tracking-tight text-[#17251f]">
                    {detail?.accountCategory ?? 'Account'}
                  </h1>
                  <p className="mt-2 text-[#667085]">
                    {selected?.accountType ?? ''}
                  </p>
                </div>
                <div className="text-left md:text-right">
                  <span className="block text-[13px] text-[#667085]">
                    Current balance
                  </span>
                  <strong
                    className={cn(
                      'text-[2rem] tracking-tight',
                      detail
                        ? balanceTone(detail.totalBalance)
                        : 'text-[#17251f]',
                    )}
                  >
                    {detail
                      ? formatMoney(detail.totalBalance, detail.currency)
                      : '—'}
                  </strong>
                </div>
              </div>
            </section>
            {accountQuery.isLoading ? (
              <p className="mt-7 rounded-[20px] bg-white px-8 py-7 text-[#667085]">
                Loading transactions…
              </p>
            ) : accountQuery.isError || !selected ? (
              <p className="mt-7 rounded-[20px] bg-white px-8 py-7 text-[#667085]">
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
