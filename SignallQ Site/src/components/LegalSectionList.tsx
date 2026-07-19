export interface LegalSection {
  title: string
  body: string
}

export function LegalSectionList({ sections }: { sections: LegalSection[] }) {
  return (
    <>
      {sections.map((sec) => (
        <div key={sec.title} className="flex flex-col gap-2">
          <div className="title-medium">{sec.title}</div>
          <div className="body-medium">{sec.body}</div>
        </div>
      ))}
    </>
  )
}
