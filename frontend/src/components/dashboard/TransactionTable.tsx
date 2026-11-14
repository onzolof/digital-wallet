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
            <h2 className="m-0 flex items-center gap-2.5 text-[1.75rem] font-semibold tracking-tight text-foreground">
              Transactions{' '}
              <Badge variant="secondary" className="min-w-7 justify-center">
                {rows.length}
              </Badge>
            </h2>
          </div>
          <label className="flex w-full items-center gap-2 rounded-[10px] border border-input pl-3 text-muted-foreground md:w-[260px] [&_svg]:size-[18px]">
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
                            ? 'bg-income-subtle text-income'
                            : 'bg-muted text-muted-foreground',
                        )}
                      >
                        {incoming ? <ArrowDownLeft /> : <ArrowUpRight />}
                      </span>
                      <span>
                        {transaction.transactionInformation}
                        <small className="block text-muted-foreground">
                          {incoming ? 'Payment received' : 'Card payment'}
                        </small>
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatDate(transaction.bookingDateTime)}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="secondary"
                      className="text-muted-foreground"
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
          <div className="px-2 py-7 text-muted-foreground">
            {query
              ? 'No transactions match your search.'
              : 'No transactions on this account.'}
          </div>
        )}
        <div className="mt-4 text-[13px] text-muted-foreground">
          Showing {rows.length}
        </div>
      </CardContent>
    </Card>
  )
}
