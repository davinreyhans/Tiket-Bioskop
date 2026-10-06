import { useState } from 'react'
import { api, describeError, useApi, type Page, type Profile } from '../../api'
import { useUser } from '../../auth'
import { ErrorMessage, Pager } from '../../ui'

export default function AdminUsersPage() {
  const me = useUser()
  const [page, setPage] = useState(0)
  const users = useApi<Page<Profile>>(`/users?page=${page}`)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function remove(user: Profile) {
    if (!window.confirm(`Hapus user "${user.username}"?`)) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      await api(`/users/${user.userId}`, { method: 'DELETE' })
      users.reload()
    } catch (e) {
      setError(describeError(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h2>Semua user</h2>
      <ErrorMessage error={error ?? users.error} />
      {users.data && (
        <table aria-busy={users.loading}>
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Role</th>
              <th>
                <span className="visually-hidden">Aksi</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {users.data.content.map((user) => (
              <tr key={user.userId}>
                <td>{user.userId}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>{user.role}</td>
                <td className="row-actions">
                  {/* no delete button on your own account, so an admin can't lock themselves out */}
                  {user.username !== me?.username && (
                    <button type="button" className="secondary" disabled={busy} onClick={() => remove(user)}>
                      Hapus
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {users.data && <Pager page={users.data.page} onChange={setPage} />}
    </>
  )
}
