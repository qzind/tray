/*
 * Node.js typescript signing example
 * Echoes the signed message and exits
 * Request: /signQz?requestToSign=dataToSign
 * Imports:
 * import crypto from 'crypto'
 * import {Request, Response} from 'express'
 * import fs from 'fs'
 */

// #########################################################
// #             WARNING   WARNING   WARNING               #
// #########################################################
// #                                                       #
// # This file is intended for demonstration purposes      #
// # only.                                                 #
// #                                                       #
// # It is the SOLE responsibility of YOU, the programmer  #
// # to prevent against unauthorized access to any signing #
// # functions.                                            #
// #                                                       #
// # Organizations that do not protect against un-         #
// # authorized signing will be black-listed to prevent    #
// # software piracy.                                      #
// #                                                       #
// # -QZ Industries, LLC                                   #
// #                                                       #
// #########################################################


const PRIVATE_KEY_PATH = `${__dirname}/private.pem`

interface IQzSignHandlerRequest {
    requestToSign: string
}

app.get('/signQz', (
    req: Request<unknown, unknown, unknown, IQzSignHandlerRequest>,
    res: Response<string | Record<string, string>>
) => {
    const {requestToSign} = req.query
    if (!requestToSign) {
        res.status(400).send({
            type: 'requestToSign',
            message: 'requestToSign is required'
        })
        return
    }
    fs.readFile(PRIVATE_KEY_PATH, 'utf-8', (error, privateKey) => {
        if (error) {
            res.status(400).send({
                type: error.name,
                message: error.message
            })
            return
        }
        const sign = crypto.createSign('SHA512')
        sign.update(requestToSign)
        const signature = sign.sign({key: privateKey}, 'base64')
        res.set('Content-Type', 'text/plain')
        res.status(200).send(signature)
    })
})