import { useMemo } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { formatMoney } from '@/components/dashboard/utils'
import type { Transaction } from '@/store/walletApi'
import { cashFlow } from './utils'

type TransactionChartProps = {
  transactions: Transaction[]
  currency?: string
}

export function TransactionChart({
  transactions,
  currency,
}: TransactionChartProps) {
  const flow = useMemo(() => cashFlow(transactions), [transactions])

  return (
    <Card className="mt-[22px]">
      <CardContent>
        <div>
          <p className="mb-2 text-xs font-semibold tracking-[0.14em] text-gray-500">
            CASH FLOW
          </p>
          <h2 className="m-0 text-[1.75rem] font-semibold tracking-tight text-[#17251f]">
            Income vs. expenses
          </h2>
          <p className="mt-2 text-[#667085]">
            A closer look at your daily activity
          </p>
        </div>
        <div className="mt-[18px] flex items-center gap-6 text-[13px] text-[#475467]">
          <span className="inline-flex items-center gap-2">
            <i className="size-2 rounded-full bg-[#1f7a4d]" />
            Income
          </span>
          <span className="inline-flex items-center gap-2">
            <i className="size-2 rounded-full bg-[#d7ddd8]" />
            Expenses
          </span>
        </div>
        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-[88px_1fr]">
          <div className="hidden h-[180px] flex-col justify-between text-right text-[11px] text-[#98a2b3] md:flex">
            <span>{formatMoney(flow.axisMax, currency)}</span>
            <span>{formatMoney(flow.axisMax * 0.75, currency)}</span>
            <span>{formatMoney(flow.axisMax * 0.5, currency)}</span>
            <span>{formatMoney(flow.axisMax * 0.25, currency)}</span>
            <span>{formatMoney(0, currency)}</span>
          </div>
          <div className="relative h-[180px]">
            <div className="absolute inset-0 flex flex-col justify-between">
              {Array.from({ length: 5 }).map((_, index) => (
                <i key={index} className="border-t border-[#eef0f2]" />
              ))}
            </div>
            <div className="relative grid h-full grid-cols-[repeat(14,minmax(0,1fr))]">
              {flow.days.map((item) => (
                <div
                  key={item.key}
                  className="flex h-full flex-col items-center"
                >
                  <div className="flex h-[calc(100%-22px)] w-full items-end justify-center gap-[3px]">
                    <span
                      className="block w-2 rounded-t-md rounded-b-sm bg-[#1f7a4d]"
                      style={{ height: `${item.income}%` }}
                      title={`Income ${formatMoney(item.incomeAmount, currency)}`}
                    />
                    <span
                      className="block w-2 rounded-t-md rounded-b-sm bg-[#d7ddd8]"
                      style={{ height: `${item.expenses}%` }}
                      title={`Expenses ${formatMoney(item.expenseAmount, currency)}`}
                    />
                  </div>
                  <span className="mt-1.5 text-[11px] text-[#98a2b3]">
                    {item.day}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div className="mt-3 text-right text-xs font-semibold tracking-widest text-[#98a2b3] md:text-right">
          {flow.monthLabel}
        </div>
      </CardContent>
    </Card>
  )
}
