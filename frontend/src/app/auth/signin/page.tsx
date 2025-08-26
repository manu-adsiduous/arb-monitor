"use client"

import { useState } from "react"
import { signIn, getSession } from "next-auth/react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Eye, EyeOff, AlertCircle, Loader2 } from "lucide-react"
import { toast } from "sonner"

export default function SignInPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const router = useRouter()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError("")

    try {
      const result = await signIn("credentials", {
        email,
        password,
        redirect: false,
      })

      if (result?.error) {
        setError("Invalid email or password")
        toast.error("Sign in failed", {
          description: "Please check your credentials and try again.",
        })
      } else {
        toast.success("Welcome back!", {
          description: "You've been successfully signed in.",
        })
        router.push("/dashboard")
        router.refresh()
      }
    } catch (error) {
      setError("Something went wrong. Please try again.")
      toast.error("Sign in failed", {
        description: "An unexpected error occurred.",
      })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen relative flex items-center justify-center p-4">
      {/* Full-width gradient background with fade effect */}
      <div className="fixed inset-0 pointer-events-none">
        {/* Base subtle background */}
        <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-br from-blue-50/30 via-purple-50/20 to-pink-50/30 dark:from-slate-900 dark:via-slate-800 dark:to-slate-900" />
        
        {/* Full-width animated gradient with horizontal fade */}
        <div className="absolute inset-0 overflow-hidden">
          {/* Top-left white enhancement for logo visibility */}
          <div className="absolute top-0 left-0 w-[40rem] h-56 z-10" 
               style={{
                 background: `radial-gradient(ellipse at top left, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.6) 20%, rgba(255, 255, 255, 0.3) 40%, rgba(255, 255, 255, 0.15) 60%, rgba(255, 255, 255, 0.05) 80%, transparent 100%)`
               }} />
          
          {/* Gradient fade overlay - strongest on left, fading to transparent on right */}
          <div className="absolute inset-0 animate-pulse" 
               style={{
                 background: `linear-gradient(to right, rgba(168, 85, 247, 0.4) 0%, rgba(59, 130, 246, 0.3) 5%, rgba(236, 72, 153, 0.25) 8%, rgba(168, 85, 247, 0.15) 10%, rgba(59, 130, 246, 0.08) 15%, rgba(236, 72, 153, 0.04) 20%, transparent 25%)`
               }} />
          
          {/* Dark mode gradient overlay */}
          <div className="absolute inset-0 animate-pulse dark:block hidden" 
               style={{
                 background: `linear-gradient(to right, rgba(147, 51, 234, 0.5) 0%, rgba(37, 99, 235, 0.35) 5%, rgba(236, 72, 153, 0.3) 8%, rgba(147, 51, 234, 0.2) 10%, rgba(37, 99, 235, 0.1) 15%, rgba(236, 72, 153, 0.05) 20%, transparent 25%)`
               }} />
          
          {/* Dark mode top-left enhancement */}
          <div className="absolute top-0 left-0 w-[40rem] h-56 dark:block hidden z-10" 
               style={{
                 background: `radial-gradient(ellipse at top left, rgba(15, 23, 42, 0.7) 0%, rgba(30, 41, 59, 0.5) 20%, rgba(51, 65, 85, 0.3) 40%, rgba(71, 85, 105, 0.15) 60%, rgba(100, 116, 139, 0.05) 80%, transparent 100%)`
               }} />
        
        {/* Floating orbs with horizontal positioning and fade */}
        <div className="absolute -top-10 -left-10 w-96 h-96 bg-gradient-to-br from-violet-500/50 via-purple-400/40 to-transparent rounded-full blur-3xl animate-float opacity-100" style={{animationDelay: '0s', animationDuration: '8s'}} />
        <div className="absolute top-1/3 left-1/4 w-80 h-80 bg-gradient-to-br from-blue-500/45 via-cyan-400/35 to-transparent rounded-full blur-3xl animate-float opacity-90" style={{animationDelay: '2s', animationDuration: '10s'}} />
        <div className="absolute bottom-1/4 -left-5 w-72 h-72 bg-gradient-to-br from-pink-500/40 via-rose-400/30 to-transparent rounded-full blur-3xl animate-float opacity-100" style={{animationDelay: '4s', animationDuration: '12s'}} />
        <div className="absolute bottom-10 left-1/3 w-60 h-60 bg-gradient-to-br from-indigo-500/35 via-purple-500/25 to-transparent rounded-full blur-2xl animate-float opacity-80" style={{animationDelay: '6s', animationDuration: '14s'}} />
        
        {/* Mid-screen floating orbs with reduced opacity */}
        <div className="absolute top-20 left-1/2 w-64 h-64 bg-gradient-to-br from-violet-400/20 via-purple-300/15 to-transparent rounded-full blur-3xl animate-float opacity-40" style={{animationDelay: '1s', animationDuration: '9s'}} />
        <div className="absolute bottom-32 left-2/5 w-48 h-48 bg-gradient-to-br from-blue-400/15 via-cyan-300/10 to-transparent rounded-full blur-2xl animate-float opacity-30" style={{animationDelay: '3s', animationDuration: '11s'}} />
        
        {/* Subtle sparkle layer that fades horizontally */}
        <div className="absolute inset-0 bg-gradient-to-r from-white/8 via-white/4 via-white/2 to-transparent animate-pulse opacity-60" style={{animationDuration: '3s'}} />
      </div>
      
      {/* Corner accent orbs - very subtle */}
      <div className="absolute -top-96 -left-96 w-96 h-96 bg-primary/8 rounded-full blur-3xl" />
      <div className="absolute -bottom-96 -right-96 w-96 h-96 bg-purple-500/3 rounded-full blur-3xl" />
    </div>
      
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        <Card className="shadow-xl border-0 glass-card relative z-10">
          <CardHeader className="text-center space-y-2">
            <div className="mx-auto w-12 h-12 bg-gradient-to-br from-primary to-purple-600 rounded-xl flex items-center justify-center mb-4">
              <span className="text-white font-bold text-xl">A</span>
            </div>
            <CardTitle className="text-2xl font-bold bg-gradient-to-r from-primary to-purple-600 bg-clip-text text-transparent">
              Welcome Back
            </CardTitle>
            <CardDescription>
              Sign in to your Arb Monitor account
            </CardDescription>
          </CardHeader>
          
          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              {error && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400 rounded-lg border border-red-200 dark:border-red-800"
                >
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {error}
                </motion.div>
              )}

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="demo@arbmonitor.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="h-11"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="demo123"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="h-11 pr-10"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Eye className="h-4 w-4 text-muted-foreground" />
                    )}
                  </Button>
                </div>
              </div>

              <div className="flex items-center justify-between text-sm">
                <Link 
                  href="/auth/forgot-password" 
                  className="text-primary hover:underline"
                >
                  Forgot password?
                </Link>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col space-y-4">
              <Button 
                type="submit" 
                className="w-full h-11 bg-gradient-to-r from-primary to-purple-600 hover:from-primary/90 hover:to-purple-600/90"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Signing in...
                  </>
                ) : (
                  "Sign In"
                )}
              </Button>

              <div className="text-center text-sm text-muted-foreground">
                Don't have an account?{" "}
                <Link href="/auth/signup" className="text-primary hover:underline font-medium">
                  Sign up
                </Link>
              </div>
            </CardFooter>
          </form>
        </Card>

        {/* Demo credentials info */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="mt-6 p-4 glass-thin rounded-2xl relative z-10"
        >
          <h3 className="font-medium text-slate-800 dark:text-white mb-2">Demo Credentials</h3>
          <div className="text-sm text-slate-700 dark:text-slate-300 space-y-1">
            <p><strong>Email:</strong> demo@arbmonitor.com</p>
            <p><strong>Password:</strong> demo123</p>
          </div>
        </motion.div>
      </motion.div>
    </div>
  )
}
