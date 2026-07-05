interface PageHeaderProps {
  title: string
  subtitle?: React.ReactNode
  actions?: React.ReactNode
}

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-[23px] font-bold tracking-tight text-heading">{title}</h1>
        {subtitle && <span className="text-[13.5px] text-muted">{subtitle}</span>}
      </div>
      {actions && <div className="flex items-center gap-2.5">{actions}</div>}
    </div>
  )
}
