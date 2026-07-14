// GH#965 — mock minimo de R2Bucket para testar uploadProviderLogo end-to-end
// sem depender de um bucket real (R2 ainda nao habilitado na conta Cloudflare,
// ver nota em wrangler.toml). Implementa so o subconjunto usado por
// provider-directory.ts (`put`).

type StoredObject = {
  body: ArrayBuffer;
  contentType: string | undefined;
};

export class FakeR2Bucket {
  objects = new Map<string, StoredObject>();

  async put(key: string, value: ArrayBuffer, options?: { httpMetadata?: { contentType?: string } }): Promise<void> {
    this.objects.set(key, {
      body: value,
      contentType: options?.httpMetadata?.contentType,
    });
  }
}
