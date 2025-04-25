export interface Node {
    id: number
    x: number
    y: number
}

export interface Link {
    id: number
    from: Node
    to: Node
    capacity: number
    speed: number
}

export interface Route {
    links: Array<Link>
}

export interface CustomEventData {
    id: number;
    name: string;
}

export interface MATSimEvent {
    time: number
    type: string
    person: string
    vehicle: string
    link: string
}
