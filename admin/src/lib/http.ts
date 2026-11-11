import { NextResponse } from "next/server";
import { ZodError, type ZodTypeAny, type z } from "zod";

export class ApiError extends Error {
  constructor(public status: number, public code: string, message?: string) {
    super(message ?? code);
  }
}

export const json = (data: unknown, status = 200) => NextResponse.json(data, { status });
export const fail = (status: number, code: string, message?: string) => NextResponse.json({ error: code, message: message ?? code }, { status });

export function errorResponse(e: unknown): NextResponse {
  if (e instanceof ApiError) return fail(e.status, e.code, e.message);
  if (e instanceof ZodError) return fail(400, "INVALID_INPUT", e.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join("; "));
  console.error("API error", e);
  return fail(500, "SERVER_ERROR");
}

export function route<C = unknown>(fn: (req: Request, ctx: C) => Promise<Response>) {
  return async (req: Request, ctx: C): Promise<Response> => {
    try {
      return await fn(req, ctx);
    } catch (e) {
      return errorResponse(e);
    }
  };
}

export async function body<S extends ZodTypeAny>(req: Request, schema: S): Promise<z.infer<S>> {
  let raw: unknown;
  try {
    raw = await req.json();
  } catch {
    throw new ApiError(400, "INVALID_JSON");
  }
  return schema.parse(raw);
}
