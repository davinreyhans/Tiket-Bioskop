import type { Page } from './api'

export function Pager({ page, onChange }: { page: Page<unknown>['page']; onChange: (page: number) => void }) {
  if (page.totalPages <= 1) {
    return null
  }
  return (
    <nav className="pager" aria-label="Halaman">
      <button type="button" disabled={page.number === 0} onClick={() => onChange(page.number - 1)}>
        ‹ Sebelumnya
      </button>
      <span>
        Halaman {page.number + 1} dari {page.totalPages}
      </span>
      <button type="button" disabled={page.number + 1 >= page.totalPages} onClick={() => onChange(page.number + 1)}>
        Berikutnya ›
      </button>
    </nav>
  )
}

export function ErrorMessage({ error }: { error?: Error | string | null }) {
  if (!error) {
    return null
  }
  return (
    <p className="error" role="alert">
      {typeof error === 'string' ? error : error.message}
    </p>
  )
}
