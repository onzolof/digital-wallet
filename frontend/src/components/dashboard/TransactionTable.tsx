import { useMemo, useState } from 'react'
import { ArrowDownLeft, ArrowUpRight, ChevronDown, Search } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Money } from '@/components/dashboard/Money'
import { formatDate, isCredit } from '@/components/dashboard/utils'
import {
  toggleSort,
  visibleTransactions,
  type TransactionSort,
} from '@/components/dashboard/transactionTableUtils'
import { cn } from '@/lib/utils'
import type { Transaction } from '@/store/walletApi'

type TransactionTableProps = {
  transactions: Transaction[]
}

export function TransactionTable({ transactions }: TransactionTableProps) {
  const [sort, setSort] = useState<TransactionSort>({
    key: 'bookingDateTime',
    direction: 'desc',
  })
  const [query, setQuery] = useState('')
  const rows = useMemo(
    () => visibleTransactions(transactions, query, sort),
    [transactions, query, sort],
  )

  return (
    <Card className="mt-[22px]">
      <CardContent>
        <div className="mb-[18px] flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
          <div>
            <h2 className="m-0 flex items-center gap-2.5 text-[1.75rem] font-semibold tracking-tight text-[#17251f]">
              Transactions{' '}
              <Badge
                variant="secondary"
                className="min-w-7 justify-center bg-[#eef2ef] text-[#17251f]"
              >
                {rows.length}
              </Badge>
            </h2>
          </div>
          <label className="flex w-full items-center gap-2 rounded-[10px] border border-[#e4e7ec] pl-3 text-[#98a2b3] md:w-[260px] [&_svg]:size-[18px]">
            <Search />
            <Input
              aria-label="Search transactions"
              placeholder="Search transactions"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="border-0 shadow-none focus-visible:ring-0"
            />
          </label>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>
                <button
                  type="button"
                  className="inline-flex cursor-pointer items-center gap-1 border-0 bg-transparent p-0 font-inherit"
                  onClick={() =>
                    setSort((current) =>
                      toggleSort(current, 'transactionInformation'),
                    )
                  }
                >
                  Transaction <ChevronDown className="size-[18px]" />
                </button>
              </TableHead>
              <TableHead>
                <button
                  type="button"
                  className="inline-flex cursor-pointer items-center gap-1 border-0 bg-transparent p-0 font-inherit"
                  onClick={() =>
                    setSort((current) => toggleSort(current, 'bookingDateTime'))
                  }
                >
                  Date <ChevronDown className="size-[18px]" />
                </button>
              </TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">
                <button
                  type="button"
                  className="inline-flex cursor-pointer items-center gap-1 border-0 bg-transparent p-0 font-inherit"
                  onClick={() =>
                    setSort((current) => toggleSort(current, 'amount'))
                  }
                >
                  Amount <ChevronDown className="size-[18px]" />
                </button>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((transaction) => {
              const incoming = isCredit(transaction.creditDebitIndicator)
              const magnitude = Math.abs(Number(transaction.amount))
              const signed = incoming ? magnitude : -magnitude
              return (
                <TableRow key={transaction.transactionId}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <span
                        className={cn(
                          'inline-flex size-[34px] items-center justify-center rounded-[10px] [&_svg]:size-[18px]',
                          incoming
                            ? 'bg-[#e7f3ec] text-[#1f7a4d]'
                            : 'bg-[#f4f5f7] text-[#475467]',
                        )}
                      >
                        {incoming ? <ArrowDownLeft /> : <ArrowUpRight />}
                      </span>
                      <span>
                        {transaction.transactionInformation}
                        <small className="block text-[#98a2b3]">
                          {incoming ? 'Payment received' : 'Card payment'}
                        </small>
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-[#667085]">
                    {formatDate(transaction.bookingDateTime)}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="secondary"
                      className="bg-[#eef2ef] text-[#667085]"
                    >
                      {transaction.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Money
                      value={signed}
                      currency={transaction.currency}
                      className="font-semibold"
                    />
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
        {rows.length === 0 && (
          <div className="px-2 py-7 text-[#667085]">
            {query
              ? 'No transactions match your search.'
              : 'No transactions on this account.'}
          </div>
        )}
        <div className="mt-4 text-[13px] text-[#667085]">
          Showing {rows.length}
        </div>
      </CardContent>
    </Card>
  )
}
