import {
  Breadcrumb as BreadcrumbNav,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb.tsx'

type BreadcrumbProps = {
  accountId: string | null
  onAccounts: () => void
}

export function Breadcrumb({ accountId, onAccounts }: BreadcrumbProps) {
  return (
    <div className="mb-7">
      <BreadcrumbNav>
        <BreadcrumbList className="text-base">
          <BreadcrumbItem>
            <BreadcrumbLink
              href="#accounts"
              onClick={(event) => {
                event.preventDefault()
                onAccounts()
              }}
            >
              Accounts
            </BreadcrumbLink>
          </BreadcrumbItem>
          {accountId && (
            <>
              <BreadcrumbSeparator />
              <BreadcrumbItem>
                <BreadcrumbPage>{accountId}</BreadcrumbPage>
              </BreadcrumbItem>
            </>
          )}
        </BreadcrumbList>
      </BreadcrumbNav>
    </div>
  )
}
