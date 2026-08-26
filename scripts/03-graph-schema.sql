CREATE TABLE IF NOT EXISTS graph_nodes (
    id          VARCHAR(50) PRIMARY KEY,
    type        VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    properties  JSONB DEFAULT '{}',
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS graph_edges (
    id          SERIAL PRIMARY KEY,
    source_id   VARCHAR(50) NOT NULL REFERENCES graph_nodes(id),
    target_id   VARCHAR(50) NOT NULL REFERENCES graph_nodes(id),
    relation    VARCHAR(50) NOT NULL,
    properties  JSONB DEFAULT '{}',
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (source_id, target_id, relation)
);

CREATE INDEX IF NOT EXISTS idx_graph_nodes_type ON graph_nodes(type);
CREATE INDEX IF NOT EXISTS idx_graph_nodes_name ON graph_nodes(name);
CREATE INDEX IF NOT EXISTS idx_graph_edges_source ON graph_edges(source_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_target ON graph_edges(target_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_relation ON graph_edges(relation);
