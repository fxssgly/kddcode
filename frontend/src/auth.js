const USERS_KEY = 'kdd_users'
const LOGGED_IN_KEY = 'kdd_logged_in'
const CURRENT_USER_KEY = 'kdd_current_user'

function isLegacyDefaultUser(user) {
  return user?.username === 'admin' && user?.password === '123456'
}

export function getUsers() {
  const saved = localStorage.getItem(USERS_KEY)
  if (!saved) return []

  try {
    const users = JSON.parse(saved)
    if (!Array.isArray(users)) return []

    const migratedUsers = users.filter((user) => !isLegacyDefaultUser(user))
    if (migratedUsers.length !== users.length) {
      saveUsers(migratedUsers)
      if (localStorage.getItem(CURRENT_USER_KEY) === 'admin') {
        logout()
      }
    }
    return migratedUsers
  } catch {
    return []
  }
}

export function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

export function isLoggedIn() {
  const currentUser = localStorage.getItem(CURRENT_USER_KEY)
  return localStorage.getItem(LOGGED_IN_KEY) === 'true'
    && Boolean(currentUser)
    && getUsers().some((user) => user.username === currentUser)
}

export function setLoggedIn(username) {
  localStorage.setItem(LOGGED_IN_KEY, 'true')
  localStorage.setItem(CURRENT_USER_KEY, username)
}

export function logout() {
  localStorage.removeItem(LOGGED_IN_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
}
