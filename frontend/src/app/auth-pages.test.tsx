import { fireEvent, render, screen } from "@testing-library/react";
import LoginPage from "@/app/(auth)/login/page";
import RegisterPage from "@/app/(auth)/register/page";

const loginMock = vi.fn();
const registerMock = vi.fn();
const saveTokenMock = vi.fn();
const setActiveProfileMock = vi.fn();

vi.mock("next/link", () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a>,
}));

vi.mock("@/lib/api", () => ({
  login: (...args: unknown[]) => loginMock(...args),
  register: (...args: unknown[]) => registerMock(...args),
}));

vi.mock("@/lib/auth", () => ({
  saveToken: (...args: unknown[]) => saveTokenMock(...args),
  setActiveProfile: (...args: unknown[]) => setActiveProfileMock(...args),
}));

describe("auth pages", () => {
  beforeEach(() => {
    loginMock.mockReset();
    registerMock.mockReset();
    saveTokenMock.mockReset();
    setActiveProfileMock.mockReset();
  });

  it("renders the login page and shows an error on failed login", async () => {
    loginMock.mockRejectedValue(new Error("invalid"));

    render(<LoginPage />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@example.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "wrong-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Login" }));

    expect(await screen.findByText("Invalid credentials. Please check your email and password.")).toBeInTheDocument();
  });

  it("renders the register page and validates mismatched passwords", async () => {
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Ada" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "ada@example.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "abc12345" } });
    fireEvent.change(screen.getByLabelText("Confirm password"), { target: { value: "different" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Passwords do not match.")).toBeInTheDocument();
  });

  it("requires an eight-character password before registering", async () => {
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Ada" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "ada@example.com" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "short" } });
    fireEvent.change(screen.getByLabelText("Confirm password"), { target: { value: "short" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Password must be at least 8 characters.")).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });
});
