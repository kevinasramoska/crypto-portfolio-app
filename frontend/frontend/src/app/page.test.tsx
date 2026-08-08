import { render, screen } from "@testing-library/react";
import Home from "@/app/page";

vi.mock("next/link", () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a>,
}));

describe("Home", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("shows registration and login calls to action to guests", () => {
    render(<Home />);

    expect(screen.getByRole("link", { name: "Get Started" })).toHaveAttribute("href", "/register");
    expect(screen.getByRole("link", { name: "Sign In" })).toHaveAttribute("href", "/login");
  });

  it("shows dashboard calls to action to signed-in users", async () => {
    window.localStorage.setItem("token", "test-token");

    render(<Home />);

    expect(await screen.findByRole("link", { name: "Open Dashboard" })).toHaveAttribute("href", "/dashboard");
    expect(screen.getByRole("link", { name: "View Dashboard" })).toHaveAttribute("href", "/dashboard");
    expect(screen.queryByRole("link", { name: "Sign In" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Get Started" })).not.toBeInTheDocument();
  });
});
