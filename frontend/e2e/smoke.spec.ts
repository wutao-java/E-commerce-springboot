import { expect, test } from "@playwright/test";

async function login(page: import("@playwright/test").Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel("用户名").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录商城" }).click();
}

async function expectNoPageOverflow(page: import("@playwright/test").Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(1);
}

async function expectProductImagesLoaded(page: import("@playwright/test").Page) {
  await expect.poll(() => page.locator(".product-card img").evaluateAll((images) =>
    images.length > 0 && images.every((image) => (image as HTMLImageElement).complete
      && (image as HTMLImageElement).naturalWidth > 0),
  )).toBe(true);
}

test("买家商城和管理员工作区可用", async ({ page }, testInfo) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "一套可以完整走通的本地商城" })).toBeVisible();
  await page.getByRole("button", { name: "注册", exact: true }).click();
  await expect(page.getByLabel("姓名")).toBeVisible();
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("auth-desktop.png"), fullPage: true });

  await login(page, "buyer", "buyer123");
  await expect(page.getByRole("heading", { name: "日常装备，精简选择" })).toBeVisible();
  await expect(page.locator(".product-card").first()).toBeVisible();
  await expectProductImagesLoaded(page);
  await expect(page.locator(".cart-panel")).toHaveCount(0);
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("buyer-desktop.png"), fullPage: true });

  await page.locator(".view-button").first().click();
  await expect(page.locator(".detail-dialog").getByRole("heading", { name: "Flow 机械键盘" })).toBeVisible();
  await page.locator(".detail-dialog").getByRole("button", { name: "关闭" }).click();
  await page.getByRole("button", { name: "将Flow 机械键盘加入购物车" }).click();
  await expect(page.locator(".cart-panel")).toHaveCount(0);
  await page.getByRole("navigation", { name: "主导航" }).getByRole("button", { name: /^购物车(?: \d+)?$/ }).click();
  await expect(page.getByRole("heading", { name: "购物车" })).toBeVisible();
  await expect(page.locator(".cart-item")).toHaveCount(1);
  await page.getByRole("button", { name: "移除Flow 机械键盘" }).click();
  await expect(page.locator(".cart-item")).toHaveCount(0);
  await page.getByRole("button", { name: "订单", exact: true }).click();
  await expect(page.getByRole("heading", { name: "我的订单" })).toBeVisible();
  await page.getByRole("button", { name: "售后", exact: true }).click();
  await expect(page.getByRole("heading", { name: "我的售后" })).toBeVisible();
  await page.getByRole("button", { name: "账户", exact: true }).click();
  await expect(page.getByRole("heading", { name: "账户与资料" })).toBeVisible();

  await page.getByRole("button", { name: "退出登录" }).click();
  await expect(page.getByLabel("用户名")).toBeVisible();
  await login(page, "admin", "admin123");
  await expect(page.getByRole("heading", { name: "商城概览" })).toBeVisible();
  await expect(page.locator(".stat-item")).toHaveCount(4);
  await expect(page.getByText("共 8 件商品")).toBeVisible();
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("admin-desktop.png"), fullPage: true });

  await page.getByRole("button", { name: "商品", exact: true }).click();
  await expect(page.getByRole("heading", { name: "商品管理" })).toBeVisible();
  await expect(page.getByText("Flow 机械键盘")).toBeVisible();
  await page.getByRole("button", { name: "订单", exact: true }).click();
  await expect(page.getByRole("heading", { name: "订单管理" })).toBeVisible();
  await page.getByRole("button", { name: "售后", exact: true }).click();
  await expect(page.getByRole("heading", { name: "售后管理" })).toBeVisible();
  await page.getByRole("button", { name: "用户", exact: true }).click();
  await expect(page.getByRole("heading", { name: "用户管理" })).toBeVisible();
  await expect(page.getByText("@buyer")).toBeVisible();
  await expect(page.locator(".data-table").getByText("商城管理员")).toHaveCount(0);
  await page.getByRole("button", { name: "新增用户" }).click();
  await expect(page.getByRole("heading", { name: "新增用户" })).toBeVisible();
  await expect(page.getByLabel("初始密码")).toBeVisible();
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("admin-users-desktop.png"), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("admin-users-mobile.png"), fullPage: true });
  await page.getByRole("button", { name: "取消" }).click();
});

test("移动端商城无页面级横向溢出", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, "buyer", "buyer123");
  await expect(page.getByRole("heading", { name: "日常装备，精简选择" })).toBeVisible();
  await expectProductImagesLoaded(page);
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("buyer-mobile.png"), fullPage: true });

  await page.getByRole("button", { name: "账户", exact: true }).click();
  await expect(page.getByRole("heading", { name: "账户与资料" })).toBeVisible();
  await expectNoPageOverflow(page);
});

test("不同订单状态显示可用的售后类型且弹窗响应式可用", async ({ page }, testInfo) => {
  await page.route("**/api/orders", async (route) => {
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        code: 0,
        message: "success",
        data: [{
          id: 998,
          orderNo: "EC-AFTER-SALE-PAID",
          userId: 1,
          status: "PAID",
          totalAmount: 239,
          receiverName: "演示买家",
          receiverPhone: "13800138000",
          shippingAddress: "上海市浦东新区 Agent 路 1 号",
          trackingNo: null,
          paidAt: "2026-09-14T10:00:00",
          shippedAt: null,
          completedAt: null,
          createdAt: "2026-09-14T09:00:00",
          updatedAt: "2026-09-14T10:00:00",
          items: [],
        }, {
          id: 999,
          orderNo: "EC-AFTER-SALE-VISUAL",
          userId: 1,
          status: "COMPLETED",
          totalAmount: 599,
          receiverName: "演示买家",
          receiverPhone: "13800138000",
          shippingAddress: "上海市浦东新区 Agent 路 1 号",
          trackingNo: "SF1234567890",
          paidAt: "2026-09-14T10:00:00",
          shippedAt: "2026-09-14T11:00:00",
          completedAt: "2026-09-14T12:00:00",
          createdAt: "2026-09-14T09:00:00",
          updatedAt: "2026-09-14T12:00:00",
          items: [],
        }],
      }),
    });
  });

  await login(page, "buyer", "buyer123");
  await page.getByRole("button", { name: "订单", exact: true }).click();
  const paidOrder = page.locator(".order-card").filter({ hasText: "EC-AFTER-SALE-PAID" });
  await paidOrder.getByRole("button", { name: "申请售后" }).click();
  await expect(page.getByRole("button", { name: "仅退款" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("button", { name: "退货退款" })).toBeDisabled();
  await page.getByRole("button", { name: "关闭" }).click();

  const completedOrder = page.locator(".order-card").filter({ hasText: "EC-AFTER-SALE-VISUAL" });
  await completedOrder.getByRole("button", { name: "申请售后" }).click();
  await expect(page.getByRole("heading", { name: "申请售后" })).toBeVisible();
  await expect(page.getByRole("button", { name: "仅退款" })).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: "退货退款" }).click();
  await expect(page.getByRole("button", { name: "退货退款" })).toHaveAttribute("aria-pressed", "true");
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("after-sale-dialog-desktop.png"), fullPage: true });

  await page.setViewportSize({ width: 390, height: 844 });
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("after-sale-dialog-mobile.png"), fullPage: true });
});
