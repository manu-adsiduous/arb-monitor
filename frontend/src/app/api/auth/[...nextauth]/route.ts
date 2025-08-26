import NextAuth from "next-auth"
import CredentialsProvider from "next-auth/providers/credentials"
import bcrypt from "bcryptjs"

// Generate a simple secret if not provided (for development only)
const nextAuthSecret = process.env.NEXTAUTH_SECRET || "fallback-secret-key-for-development-only"

const handler = NextAuth({
  providers: [
    CredentialsProvider({
      name: "credentials",
      credentials: {
        email: { label: "Email", type: "email" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          return null
        }

        try {
          // TODO: Replace with actual database call
          // For now, we'll use a mock user for development
          const mockUser = {
            id: "1",
            email: "demo@arbmonitor.com",
            password: await bcrypt.hash("demo123", 12), // hashed "demo123"
            name: "Demo User"
          }

          if (credentials.email === mockUser.email) {
            const isPasswordValid = await bcrypt.compare(
              credentials.password,
              mockUser.password
            )

            if (isPasswordValid) {
              return {
                id: mockUser.id,
                email: mockUser.email,
                name: mockUser.name,
              }
            }
          }

          return null
        } catch (error) {
          console.error("Auth error:", error)
          return null
        }
      }
    })
  ],
  secret: nextAuthSecret,
  session: {
    strategy: "jwt",
    maxAge: 30 * 24 * 60 * 60, // 30 days
    updateAge: 24 * 60 * 60, // Update session every 24 hours
  },
  jwt: {
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },
  cookies: {
    sessionToken: {
      name: `next-auth.session-token`,
      options: {
        httpOnly: true,
        sameSite: 'lax',
        path: '/',
        secure: process.env.NODE_ENV === 'production'
      }
    }
  },
  pages: {
    signIn: "/auth/signin",
  },
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.id = user.id
      }
      return token
    },
    async session({ session, token }) {
      if (token && session.user) {
        session.user.id = token.id as string
      }
      return session
    },
  },
})

export { handler as GET, handler as POST }
