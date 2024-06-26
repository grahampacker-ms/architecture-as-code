import { readFileSync, writeFileSync } from 'node:fs';
import * as graphviz from 'graphviz-cli';
import { visualizeInstantiation, visualizePattern } from './visualize';

jest.mock('node:fs', () => {
    return {
        readFileSync: jest.fn(),
        writeFileSync: jest.fn()
    };
});

jest.mock('graphviz-cli', () => ({
    __esModule: true,
    ...jest.requireActual('graphviz-cli')
}));

jest.mock('./calmToDot.js', () => jest.fn());

jest.mock('../helper.js', () => {
    return {
        initLogger: () => {
            return {
                info: jest.fn(),
                debug: jest.fn(),
                error: jest.fn()
            };
        }
    };
});

describe('visualize instantiation', () => {
    beforeEach(() => {
        (readFileSync as jest.Mock).mockReturnValue(`
        {
            "nodes": [],
            "relationships": []
        }
        `);
    });


    afterEach(() => {
        jest.resetAllMocks();
    });

    it('reads from the given input file', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockResolvedValue('<svg></svg>');

        await visualizeInstantiation('./input.json', './output.svg', false);
        expect(readFileSync).toHaveBeenCalledWith('./input.json', 'utf-8');
    });

    it('writes to the given output file', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockResolvedValue('<svg></svg>');

        await visualizeInstantiation('./input.json', './output.svg', false);
        expect(writeFileSync).toHaveBeenCalledWith('./output.svg', '<svg></svg>');
    });

    it('doesnt write if an error is thrown', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockRejectedValue(new Error());

        await visualizeInstantiation('./input.json', './output.svg', false);
        expect(writeFileSync).not.toHaveBeenCalled();
    });
});

describe('visualize pattern', () => {
    beforeEach(() => {
        (readFileSync as jest.Mock).mockReturnValue(`
        {
            "properties": {
                "nodes": {
                    "prefixItems": []
                },
                "relationships": {
                    "prefixItems": []
                }
            },
            "required": [
                "nodes",
                "relationships"
            ]
        }
        `);
    });


    afterEach(() => {
        jest.resetAllMocks();
    });

    it('reads from the given input file', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockResolvedValue('<svg></svg>');

        await visualizePattern('./input.json', './output.svg', false);
        expect(readFileSync).toHaveBeenCalledWith('./input.json', 'utf-8');
    });

    it('writes to the given output file', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockResolvedValue('<svg></svg>');

        await visualizePattern('./input.json', './output.svg', false);
        expect(writeFileSync).toHaveBeenCalledWith('./output.svg', '<svg></svg>');
    });

    it('doesnt write if an error is thrown', async () => {
        jest.spyOn(graphviz, 'renderGraphFromSource').mockRejectedValue(new Error());

        await visualizePattern('./input.json', './output.svg', false);
        expect(writeFileSync).not.toHaveBeenCalled();
    });
});